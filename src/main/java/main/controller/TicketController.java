package main.controller;

import main.model.Ticket;
import main.service.QrCodeService;
import main.service.TicketService;
import main.service.UserService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final QrCodeService qrCodeService;
    private final UserService userService;

    public TicketController(TicketService ticketService,
                            QrCodeService qrCodeService,
                            UserService userService) {
        this.ticketService = ticketService;
        this.qrCodeService = qrCodeService;
        this.userService = userService;
    }

    @GetMapping("/{code}/qr")
    public ResponseEntity<byte[]> renderTicketQr(@PathVariable String code, Principal principal) {
        Ticket ticket = ticketService.getTicketForQr(code, userService.getByEmail(principal.getName()));
        byte[] qrImage = qrCodeService.generatePng(ticket.getCode());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setCacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).cachePrivate());
        return ResponseEntity.ok()
                .headers(headers)
                .body(qrImage);
    }
}

