package com.hogaria.notification.controller;

import com.hogaria.notification.dto.NotificationSettingDto;
import com.hogaria.notification.dto.UpdateNotificationSettingRequest;
import com.hogaria.notification.service.NotificationSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.web.bind.annotation.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.parameters.*;
import io.swagger.v3.oas.annotations.media.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/notification-settings")
@Tag(name = "Notifications", description = "Configuracion de notificaciones por usuario y evento")
public class NotificationSettingController {

    private final NotificationSettingService service;

    public NotificationSettingController(NotificationSettingService service) {
        this.service = service;
    }

    @Operation(summary = "Listar settings", description = "Devuelve la configuracion de notificaciones de un usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de settings",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = NotificationSettingDto.class))))
    })
    @GetMapping
    public List<NotificationSettingDto> list(
            @Parameter(description = "ID del usuario", required = true, in = ParameterIn.QUERY)
            @RequestParam("user_id") Long userId
    ) {
        return service.listByUser(userId);
    }

    @Operation(summary = "Actualizar setting", description = "Modifica la configuracion de un notification-setting existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Setting actualizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NotificationSettingDto.class))),
            @ApiResponse(responseCode = "404", description = "Setting no encontrado")
    })
    @PutMapping("/{id}")
    public NotificationSettingDto update(
            @Parameter(description = "ID del setting", required = true, in = ParameterIn.PATH)
            @PathVariable Long id,
            @RequestBody UpdateNotificationSettingRequest dto
    ) {
        return service.update(id, dto);
    }
}
