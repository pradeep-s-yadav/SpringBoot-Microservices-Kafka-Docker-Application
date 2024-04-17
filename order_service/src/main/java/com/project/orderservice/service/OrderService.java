package com.project.orderservice.service;

import com.project.orderservice.dto.InventoryResponse;
import com.project.orderservice.dto.OrderItemsDto;
import com.project.orderservice.dto.OrderRequest;
import com.project.orderservice.event.OrderEvent;
import com.project.orderservice.model.Order;
import com.project.orderservice.model.OrderItems;
import com.project.orderservice.repository.OrderRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public String placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderItems> orderItems = orderRequest.getOrderItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();
        order.setOrderItemsList(orderItems);

        List<String> skuCodes = order.getOrderItemsList().stream()
                .map(OrderItems::getSkuCode)
                .toList();

        // webclient calls the inventory service
        // to check whether the product is in stock or not
        InventoryResponse[] inventoryResponse = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean isInStock = Arrays.stream(inventoryResponse)
                .allMatch(response -> response.isInStock());

        if(isInStock){
            orderRepository.save(order);
            kafkaTemplate.send("notificationTopic", new OrderEvent(order.getOrderNumber()));
            return "Order placed successfully";
        } else{
            throw new IllegalArgumentException
                    ("Product is not in stock. Please check again later!");
        }

    }

    private OrderItems mapToDto(OrderItemsDto orderItemsDto) {
        OrderItems orderItems = new OrderItems();
        orderItems.setQuantity(orderItemsDto.getQuantity());
        orderItems.setSkuCode(orderItemsDto.getSkuCode());
        orderItems.setPrice(orderItemsDto.getPrice());
        return orderItems;
    }


}
