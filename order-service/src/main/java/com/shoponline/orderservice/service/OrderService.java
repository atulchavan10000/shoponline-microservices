package com.shoponline.orderservice.service;


import com.shoponline.orderservice.dto.InventoryResponse;
import com.shoponline.orderservice.dto.OrderLineItemsDto;
import com.shoponline.orderservice.dto.OrderRequest;
import com.shoponline.orderservice.model.Order;
import com.shoponline.orderservice.model.OrderLineItems;
import com.shoponline.orderservice.repository.OrderRepository;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Span;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;

    public String placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();
        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        log.info("Calling inventory service");

        Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");

        try(Tracer.SpanInScope spanInScope = tracer.withSpan(inventoryServiceLookup.start())){
            // call inventory service, and place order only if the product is in stock
            InventoryResponse[]  inventoryResponseArray =  webClientBuilder.build().get()
                    .uri("http://inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode",skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();
            //if(inventoryResponseArray.length == 0) throw new IllegalArgumentException("Products not available");
            boolean allProductsInStock = Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse::getIsInStock);

            if(allProductsInStock) { // order will be placed only if all the products are in stock
                orderRepository.save(order);
                return "Order Placed Successfully!";
            }else{
                throw new IllegalArgumentException("Product is out of stock, please try again later");
            }
        }finally {
            inventoryServiceLookup.end();
        }

    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
