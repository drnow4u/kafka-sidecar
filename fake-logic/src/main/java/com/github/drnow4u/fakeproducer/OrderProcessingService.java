package com.github.drnow4u.fakeproducer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class OrderProcessingService {

    public Map<String, Object> calculateOrderDiscount(Order order) {
        Map<String, Object> discountInfo = new HashMap<>();
        BigDecimal discountPercent = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        String discountReason = "No discount applicable";

        // Apply discount based on order value
        if (order.getTotalAmount().compareTo(BigDecimal.valueOf(5000)) >= 0) {
            discountPercent = BigDecimal.valueOf(15);
            discountReason = "VIP Customer - 15% discount";
        } else if (order.getTotalAmount().compareTo(BigDecimal.valueOf(2000)) >= 0) {
            discountPercent = BigDecimal.valueOf(10);
            discountReason = "High Value Order - 10% discount";
        } else if (order.getTotalAmount().compareTo(BigDecimal.valueOf(1000)) >= 0) {
            discountPercent = BigDecimal.valueOf(5);
            discountReason = "Volume Purchase - 5% discount";
        }

        if (discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = order.getTotalAmount()
                .multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        }

        discountInfo.put("discountPercent", discountPercent);
        discountInfo.put("discountAmount", discountAmount);
        discountInfo.put("finalAmount", order.getTotalAmount().subtract(discountAmount));
        discountInfo.put("reason", discountReason);

        log.info("Order {} discount calculated: {} ({})", 
            order.getOrderId(), discountPercent, discountReason);
        
        return discountInfo;
    }

    public Map<String, Object> analyzeOrderComposition(Order order) {
        Map<String, Object> composition = new HashMap<>();

        // Calculate item statistics
        long itemCount = order.getItems().size();
        int totalQuantity = order.getItems().stream()
            .mapToInt(CartItem::getQuantity)
            .sum();

        BigDecimal minPrice = order.getItems().stream()
            .map(CartItem::getPrice)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice = order.getItems().stream()
            .map(CartItem::getPrice)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        BigDecimal avgPrice = minPrice.add(maxPrice)
            .divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);

        composition.put("itemCount", itemCount);
        composition.put("totalQuantity", totalQuantity);
        composition.put("minPrice", minPrice);
        composition.put("maxPrice", maxPrice);
        composition.put("averagePrice", avgPrice);
        composition.put("priceRange", minPrice + " - " + maxPrice);

        log.debug("Order {} composition: {} items, {} units", 
            order.getOrderId(), itemCount, totalQuantity);
        
        return composition;
    }

    public Map<String, Object> estimateShippingCost(Order order) {
        Map<String, Object> shipping = new HashMap<>();

        // Calculate shipping based on quantity and value
        int totalQuantity = order.getItems().stream()
            .mapToInt(CartItem::getQuantity)
            .sum();

        BigDecimal baseShipping = BigDecimal.valueOf(10);
        BigDecimal quantityCharge = BigDecimal.valueOf(totalQuantity).multiply(BigDecimal.valueOf(0.5));
        BigDecimal shippingCost = baseShipping.add(quantityCharge);

        // Free shipping for orders over $100
        if (order.getTotalAmount().compareTo(BigDecimal.valueOf(100)) >= 0) {
            shippingCost = BigDecimal.ZERO;
            shipping.put("shippingType", "FREE_SHIPPING");
        } else {
            shipping.put("shippingType", "STANDARD");
        }

        shipping.put("baseCharge", baseShipping);
        shipping.put("quantityCharge", quantityCharge);
        shipping.put("totalShippingCost", shippingCost);
        shipping.put("estimatedDelivery", "3-5 business days");

        log.info("Order {} shipping: ${}", order.getOrderId(), shippingCost);
        
        return shipping;
    }

    public Map<String, Object> calculateOrderProfit(Order order) {
        Map<String, Object> profit = new HashMap<>();

        // Simple profit calculation (assume 40% cost of goods sold)
        BigDecimal cogs = order.getTotalAmount().multiply(BigDecimal.valueOf(0.4));
        BigDecimal grossProfit = order.getTotalAmount().subtract(cogs);
        BigDecimal profitMargin = grossProfit
            .divide(order.getTotalAmount(), 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

        profit.put("totalRevenue", order.getTotalAmount());
        profit.put("costOfGoodsSold", cogs);
        profit.put("grossProfit", grossProfit);
        profit.put("profitMargin%", profitMargin);

        log.debug("Order {} profit analysis: Margin: {}%", order.getOrderId(), profitMargin);
        
        return profit;
    }

    public boolean isHighValueOrder(Order order) {
        return order.getTotalAmount().compareTo(BigDecimal.valueOf(1000)) >= 0;
    }

    public boolean isLowValueOrder(Order order) {
        return order.getTotalAmount().compareTo(BigDecimal.valueOf(100)) < 0;
    }

    public String categorizeOrder(Order order) {
        if (isHighValueOrder(order)) {
            return "PREMIUM";
        } else if (order.getTotalAmount().compareTo(BigDecimal.valueOf(500)) >= 0) {
            return "STANDARD";
        } else if (isLowValueOrder(order)) {
            return "ECONOMY";
        }
        return "REGULAR";
    }
}

