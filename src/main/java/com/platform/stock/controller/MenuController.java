package com.platform.stock.controller;

import com.platform.stock.dto.MenuResponse;
import com.platform.stock.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "메뉴 API", description = "메뉴 조회")
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "메뉴 상세 조회")
    @GetMapping("/{menuId}")
    public MenuResponse getMenu(@PathVariable Long menuId) {
        return menuService.getMenu(menuId);
    }

    @Operation(summary = "전체 메뉴 조회")
    @GetMapping
    public List<MenuResponse> getAllMenus() {
        return menuService.getAllMenus();
    }
}
