package com.inventario_hardware.inventario;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RRController {

    @GetMapping("/responsiva/equipos")
    public String responsivas() {
        return "responsivas"; // busca templates/responsivas.html
    }
}
