package com.exa.order.controller;


import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.exa.order.config.AlipayConfig;
import com.exa.order.pojo.Order;
import com.exa.order.service.PayService;
import entity.Result;
import entity.StatusCode;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import util.IdWorker;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


@RestController
public class PayController {

    @Autowired
    private PayService payService;


    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private IdWorker idWorker;


    /*
    * 支付宝扫码支付
    * */
    @RequestMapping(value = "/pay",method = RequestMethod.GET)
    public void test(HttpServletResponse response) throws IOException, AlipayApiException {
        response.setContentType("text/html;charset=utf-8");

        PrintWriter out = response.getWriter();
        AlipayTradePagePayRequest aliPayRequest = new AlipayTradePagePayRequest();
        aliPayRequest.setReturnUrl(alipayConfig.return_url);
        aliPayRequest.setNotifyUrl(alipayConfig.notify_url);
        Order order = new Order();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String code= RandomStringUtils.randomNumeric(6);
        order.setOrderid(sdf.format(new Date())+code);
        order.setPaymoney(111.11);
        String subject = "Test";
        System.out.println("商户订单号:"+order.getOrderid());
        /*
        * out_trade_no 商户订单号
        * total_amount 订单总金额
        * subject 订单标题
        * product_code 销售产品码
        * 必填，其他可选
        * 如
        * timeout_express 超时时间 10m 十分钟
        * */
        aliPayRequest.setBizContent("{\"out_trade_no\":\""+ order.getOrderid() +"\","
                + "\"total_amount\":\""+ order.getPaymoney() +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"timeout_express\":\"10m\","
                //电脑网站支付FAST_INSTANT_TRADE_PAY，不同支付不同码
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");
        String result = null;
        AlipayClient apAlipayClient
                = new DefaultAlipayClient(alipayConfig.gatewayUrl,
                alipayConfig.app_id,alipayConfig.merchant_private_key,"json",
                alipayConfig.charset,alipayConfig.alipay_public_key,alipayConfig.sign_type);
        AlipayTradePagePayResponse aliPayResponse= apAlipayClient.pageExecute(aliPayRequest);
        result = aliPayResponse.getBody();
        out.println(result);
        System.out.println("pay:"+result);
    }


    /*
    * 支付宝扫码支付成功返回，一般只做展示
    * 业务逻辑处理请勿在该执行
    * */
    @RequestMapping(value = "/return",method = RequestMethod.GET)
    public void Payreturn(HttpServletResponse response,HttpServletRequest request) throws IOException {
        System.out.println("return");
        response.setContentType("text/html;charset=utf-8");

        PrintWriter out = response.getWriter();
        //获取支付宝GET过来反馈信息
        Map<String,String> params = new HashMap<>();
        Map<String,String[]> requestParams = request.getParameterMap();
        for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用
            try {
                valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            params.put(name, valueStr);
        }

        boolean signVerified = false; //调用SDK验证签名
        try {
            signVerified = AlipaySignature.rsaCheckV1(params, alipayConfig.alipay_public_key, alipayConfig.charset, alipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        //——请在这里编写您的程序（以下代码仅作参考）——
        if(signVerified) {

            String out_trade_no = null;
            String trade_no = null;
            String total_amount = null;
            try {
                //商户订单号
                out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"),"UTF-8");

                //支付宝交易号
                trade_no = new String(request.getParameter("trade_no").getBytes("ISO-8859-1"),"UTF-8");

                //付款金额
                total_amount = new String(request.getParameter("total_amount").getBytes("ISO-8859-1"),"UTF-8");



            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            out.println("支付宝交易号:"+trade_no+
                    "<br/>商户订单号:"+out_trade_no+
                    "<br/>交易金额:"+total_amount);
        }else {
            out.println("验签失败");
        }
    }


    /*
    * 异步通知地址 notify_url，通过 POST 请求的形式将支付结果作为参数通知到商户系统
    * 程序执行完后必须通过 PrintWriter 类打印输出"success"（不包含引号）。
    * 如果商户反馈给支付宝的字符不是 success 这7个字符，支付宝服务器会不断重发通知，直到超过 24 小时 22 分钟。一般情况下，25 小时以内完成 8 次通知（通知的间隔频率一般是：4m,10m,10m,1h,2h,6h,15h）；
    * 该页面不能在本机电脑测试，请到服务器上做测试。请确保外部可以访问该页面。
    * 如果没有收到该页面返回的 success
    * 建议该页面只做支付成功的业务逻辑处理，退款的处理请以调用退款查询接口的结果为准。
    * */
    @RequestMapping(value = "/notify", method = RequestMethod.POST)
    public void notify(HttpServletResponse response,HttpServletRequest request) throws IOException, AlipayApiException {
        System.out.println("notify");
        response.setContentType("text/html;charset=utf-8");

        PrintWriter out = response.getWriter();
        Map<String,String> params = new HashMap<String,String>();
        Map<String,String[]> requestParams = request.getParameterMap();
        for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用
            valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(name, valueStr);
        }

        boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayConfig.alipay_public_key, alipayConfig.charset, alipayConfig.sign_type); //调用SDK验证签名

        //——请在这里编写您的程序（以下代码仅作参考）——

	/* 实际验证过程建议商户务必添加以下校验：
	1、需要验证该通知数据中的out_trade_no是否为商户系统中创建的订单号，
	2、判断total_amount是否确实为该订单的实际金额（即商户订单创建时的金额），
	3、校验通知中的seller_id（或者seller_email) 是否为out_trade_no这笔单据的对应的操作方（有的时候，一个商户可能有多个seller_id/seller_email）
	4、验证app_id是否为该商户本身。
	*/
        if(signVerified) {//验证成功
            //商户订单号
            String out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"),"UTF-8");

            //支付宝交易号
            String trade_no = new String(request.getParameter("trade_no").getBytes("ISO-8859-1"),"UTF-8");

            //交易状态
            String trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"),"UTF-8");

            /*
            * 交易完成状态，不能再退款
            * 如果商品不允许退款，交易成功状态变为交易完成状态
            * 退款日期超过可退款期限后（如三个月可退款），支付宝系统订单转为交易完成
            * */
            if(trade_status.equals("TRADE_FINISHED")){
                //判断该笔订单是否在商户网站中已经做过处理
                //如果没有做过处理，根据订单号（out_trade_no）在商户网站的订单系统中查到该笔订单的详细，并执行商户的业务程序
                //如果有做过处理，不执行商户的业务程序

                //注意：
                //退款日期超过可退款期限后（如三个月可退款），支付宝系统发送该交易状态通知
            }
            /*
            * 交易成功状态，三个月内可退款
            * */
            else if (trade_status.equals("TRADE_SUCCESS")){
                //判断该笔订单是否在商户网站中已经做过处理
                //如果没有做过处理，根据订单号（out_trade_no）在商户网站的订单系统中查到该笔订单的详细，并执行商户的业务程序
                //如果有做过处理，不执行商户的业务程序

                //注意：
                //付款完成后，支付宝系统发送该交易状态通知
            }
            System.out.println("success");
            out.println("success");

        }else {//验证失败
            System.out.println("fail");
            out.println("fail");

            //调试用，写文本函数记录程序运行情况是否正常
            String sWord = AlipaySignature.getSignCheckContentV1(params);
            alipayConfig.logResult(sWord);
        }
    }


    /*
    * 支付宝交易查询
    * */
    @RequestMapping(value = "/query/{out_trade_no}",method = RequestMethod.GET)
    public Result query(@PathVariable String out_trade_no) throws AlipayApiException {
        // 初始化的AlipayClient
        AlipayClient apAlipayClient
                = new DefaultAlipayClient(alipayConfig.gatewayUrl,
                alipayConfig.app_id,alipayConfig.merchant_private_key,"json",
                alipayConfig.charset,alipayConfig.alipay_public_key,alipayConfig.sign_type);
        AlipayTradeQueryRequest alipayRequest = new AlipayTradeQueryRequest();
        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\"}");
        AlipayTradeQueryResponse alipayResponse = apAlipayClient.execute(alipayRequest);
        if("40004".equals(alipayResponse.getCode())){
            return new Result(false, StatusCode.OPERATION_FAIL.getCode(),"查询失败,请输入正确的商品订单码");
        }
        System.out.println(alipayResponse.getBody());
        Map<String,String> map = new HashMap<>();
        map.put("out_trade_no",alipayResponse.getOutTradeNo());
        map.put("trade_no",alipayResponse.getTradeNo());
        map.put("trade_status",alipayResponse.getTradeStatus());
        map.put("total_amount",alipayResponse.getTotalAmount());
        if(alipayResponse.getSendPayDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            map.put("send_pay_date", sdf.format(alipayResponse.getSendPayDate()));
        }
        else
            map.put("send_pay_date",null);
        System.out.println("支付宝交易号:"+map.get("trade_no"));
        System.out.println("商家订单号:"+map.get("out_trade_no"));
        System.out.println("交易状态码:"+map.get("trade_status"));
        System.out.println("交易的订单金额:"+map.get("total_amount"));
        System.out.println("本次交易打款给卖家的时间:"+map.get("send_pay_date"));
        return new Result(true, StatusCode.SUCCESS.getCode(),"查询成功",map);

    }

    /*
    * 支付宝退款请求
    * out_trade_no 订单号
    * out_request_no 退款请求号，部分退款的请求号不同，全额退款，请求和为创建交易时的外部交易号
    * */
    @RequestMapping(value = "/refund/{out_trade_no}/{out_request_no}/{refund_amount}",method = RequestMethod.GET)
    public Result refund(@PathVariable String out_trade_no,@PathVariable String out_request_no, @PathVariable String refund_amount) throws AlipayApiException {
        // 初始化的AlipayClient
        AlipayClient apAlipayClient
                = new DefaultAlipayClient(alipayConfig.gatewayUrl,
                alipayConfig.app_id,alipayConfig.merchant_private_key,"json",
                alipayConfig.charset,alipayConfig.alipay_public_key,alipayConfig.sign_type);
        //设置请求参数
        AlipayTradeRefundRequest alipayRequest = new AlipayTradeRefundRequest();
        //退款的原因说明
        String refund_reason = "test";
        /*
        * refund_amount 退款金额
        * refund_reason 退款原因
        * */
        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"refund_amount\":\""+ refund_amount +"\","
                + "\"refund_reason\":\""+ refund_reason +"\","
                + "\"out_request_no\":\""+ out_request_no +"\"}");

        AlipayTradeRefundResponse aliPayResponse = apAlipayClient.execute(alipayRequest);
        //请求
        String result = aliPayResponse.getBody();
        Map<String,String> map = new HashMap<>();
        map.put("out_trade_no",aliPayResponse.getOutTradeNo());
        map.put("trade_no",aliPayResponse.getTradeNo());
        map.put("fund_change",aliPayResponse.getFundChange());
        map.put("refund_fee",aliPayResponse.getRefundFee());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        map.put("gmt_refund_pay",sdf.format(aliPayResponse.getGmtRefundPay()));
        System.out.println("支付宝交易号:"+map.get("trade_no"));
        System.out.println("商家订单号:"+map.get("out_trade_no"));
        System.out.println("本次退款是否发生了资金变化:"+map.get("fund_change"));
        System.out.println("退款总金额:"+map.get("refund_fee"));
        System.out.println("退款支付时间:"+map.get("gmt_refund_pay"));
        return new Result(true, StatusCode.SUCCESS.getCode(),"退款成功",map);

    }
    /*
    * 支付宝退款请求查询
    * out_trade_no 订单号
    * out_request_no 退款请求号，部分退款的请求号不同，全额退款，请求和为创建交易时的外部交易号
    * */
    @RequestMapping(value = "/refund/query/{out_trade_no}/{out_request_no}",method = RequestMethod.GET)
    public Result refund_query(@PathVariable String out_trade_no,@PathVariable String out_request_no) throws AlipayApiException {
        // 初始化的AlipayClient
        AlipayClient apAlipayClient
                = new DefaultAlipayClient(alipayConfig.gatewayUrl,
                alipayConfig.app_id,alipayConfig.merchant_private_key,"json",
                alipayConfig.charset,alipayConfig.alipay_public_key,alipayConfig.sign_type);

        //设置请求参数
        AlipayTradeFastpayRefundQueryRequest alipayRequest = new AlipayTradeFastpayRefundQueryRequest();


        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                +"\"out_request_no\":\""+ out_request_no +"\"}");

        AlipayTradeFastpayRefundQueryResponse alipayResponse =  apAlipayClient.execute(alipayRequest);

        if(!"10000".equals(alipayResponse.getCode())){
            return new Result(false, StatusCode.OPERATION_FAIL.getCode(),"查询失败,请输入正确的商品订单码");
        };

        String refund_amount = alipayResponse.getRefundAmount();
        System.out.println(refund_amount);
        if(refund_amount == null)
            return new Result(false, StatusCode.OPERATION_FAIL.getCode(),"退款失败，请重试");
        else
            return new Result(true, StatusCode.SUCCESS.getCode(),"退款成功");

    }


}
