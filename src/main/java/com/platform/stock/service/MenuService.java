package com.platform.stock.service;

import com.platform.stock.domain.Menu;
import com.platform.stock.dto.MenuResponse;
import com.platform.stock.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    @Transactional(readOnly = true)
    public MenuResponse getMenu(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴입니다: " + menuId));
        return MenuResponse.from(menu);
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> getAllMenus() {
        return menuRepository.findAll().stream()
            .map(MenuResponse::from)
            .toList();
    }
}
