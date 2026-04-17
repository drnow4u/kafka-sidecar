package com.github.drnow4u.fakeproducer.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Shipment implements Serializable {
    private static final long serialVersionUID = 1L;

    private String shipmentId;
    private String orderId;
    private String customerId;
    private String trackingNumber;
    private String carrier;
    private String status;
    private String shippingAddress;
    private LocalDateTime createdAt;
    private LocalDateTime estimatedDelivery;
    private Integer itemCount;
    private String shippingMethod;

    public Shipment() {
    }

    public Shipment(String shipmentId, String orderId, String customerId, String trackingNumber,
                    String carrier, String status, String shippingAddress, LocalDateTime createdAt,
                    LocalDateTime estimatedDelivery, Integer itemCount, String shippingMethod) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.status = status;
        this.shippingAddress = shippingAddress;
        this.createdAt = createdAt;
        this.estimatedDelivery = estimatedDelivery;
        this.itemCount = itemCount;
        this.shippingMethod = shippingMethod;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getEstimatedDelivery() {
        return estimatedDelivery;
    }

    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) {
        this.estimatedDelivery = estimatedDelivery;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public String getShippingMethod() {
        return shippingMethod;
    }

    public void setShippingMethod(String shippingMethod) {
        this.shippingMethod = shippingMethod;
    }

    @Override
    public String toString() {
        return "Shipment{" +
                "shipmentId='" + shipmentId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", trackingNumber='" + trackingNumber + '\'' +
                ", carrier='" + carrier + '\'' +
                ", status='" + status + '\'' +
                ", shippingAddress='" + shippingAddress + '\'' +
                ", createdAt=" + createdAt +
                ", estimatedDelivery=" + estimatedDelivery +
                ", itemCount=" + itemCount +
                ", shippingMethod='" + shippingMethod + '\'' +
                '}';
    }
}
