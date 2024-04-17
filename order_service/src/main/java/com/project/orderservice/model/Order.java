package com.project.orderservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;


@Entity
@Table(name="orders")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long Id;
    private String orderNumber;
    @OneToMany( cascade = CascadeType.ALL)
    private List<OrderItems> orderItemsList;




}
