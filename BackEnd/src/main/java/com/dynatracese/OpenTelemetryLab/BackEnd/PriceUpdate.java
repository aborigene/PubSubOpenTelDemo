package com.dynatracese.OpenTelemetryLab.BackEnd;

public class PriceUpdate {
    private String product;
    private Double price;
    private String requestId;

    public PriceUpdate(String product, Double price, String requestId){
        this.product = product;
        this.price = price;
        this.requestId = requestId;
    }

    public Double getPrice() {
        return price;
    }

    public String getProduct() {
        return product;
    }
    
    public String getRequestId() {
        return requestId;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
