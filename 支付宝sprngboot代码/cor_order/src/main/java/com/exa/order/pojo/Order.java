package com.exa.order.pojo;


import lombok.Data;

//import javax.persistence.Entity;
//import javax.persistence.Id;
//import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
 * 实体类
 * @author Administrator
 *
 */
//@Entity
//@Table(name="cor_order")
@Data
public class Order implements Serializable {

//    @Id
    private String id;//ID

    private String orderid; //订单号ID
    private String userid; //用户ID
    private Double paymoney; //支付金额
    private String paytype; //支付类型
    private String payplatform; //支付平台
    private String status; //订单状态
    private Date paytime; //支付时间
    private Date sendtime; //发货时间
    private Date endtime; //交易完成时间
    private Date closetime; //交易关闭时间
    private Date createtime; //创建时间
    private Date updatetime; //更新时间

}
