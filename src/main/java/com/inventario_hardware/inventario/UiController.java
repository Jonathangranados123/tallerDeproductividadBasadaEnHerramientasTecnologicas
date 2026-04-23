package com.inventario_hardware.inventario;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UiController {


    @GetMapping("/ui/equipos")
    public String equipos() {
        return "equipos"; // busca templates/equipos.html
    }
}
