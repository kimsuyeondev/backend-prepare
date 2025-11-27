package com.platform.stock.dto;

import com.platform.stock.domain.Menu;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MenuResponse {

    private Long menuId;
    private String name;
    private Integer price;
    private Integer stock;

    public static MenuResponse from(Menu menu) {
        return MenuResponse.builder()
            .menuId(menu.getId())
            .name(menu.getName())
            .price(menu.getPrice())
            .stock(menu.getStock())
            .build();
    }
}
