package com.smoothoperators.api.controller;

import com.smoothoperators.api.dto.OperatorDto;
import com.smoothoperators.api.service.OperatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;

@RestController
@RequestMapping("/api/v1/operators")
@Validated
public class OperatorController {

    @Autowired
    private OperatorService operatorService;

    @GetMapping
    public ResponseEntity<Page<OperatorDto>> getAllOperators(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page number must be 0 or greater") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 100, message = "Page size cannot exceed 100") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name) {
        
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        Page<OperatorDto> operators = operatorService.getAllOperators(pageable, status, name);
        return ResponseEntity.ok(operators);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OperatorDto> getOperatorById(
            @PathVariable @Positive(message = "Operator ID must be a positive number") Long id) {
        OperatorDto operator = operatorService.getOperatorById(id);
        return ResponseEntity.ok(operator);
    }

    @PostMapping
    public ResponseEntity<OperatorDto> createOperator(@Valid @RequestBody OperatorDto operatorDto) {
        OperatorDto createdOperator = operatorService.createOperator(operatorDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOperator);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OperatorDto> updateOperator(
            @PathVariable @Positive(message = "Operator ID must be a positive number") Long id, 
            @Valid @RequestBody OperatorDto operatorDto) {
        OperatorDto updatedOperator = operatorService.updateOperator(id, operatorDto);
        return ResponseEntity.ok(updatedOperator);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OperatorDto> partialUpdateOperator(
            @PathVariable @Positive(message = "Operator ID must be a positive number") Long id, 
            @RequestBody OperatorDto operatorDto) {
        OperatorDto updatedOperator = operatorService.partialUpdateOperator(id, operatorDto);
        return ResponseEntity.ok(updatedOperator);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOperator(
            @PathVariable @Positive(message = "Operator ID must be a positive number") Long id) {
        operatorService.deleteOperator(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OperatorDto> updateOperatorStatus(
            @PathVariable @Positive(message = "Operator ID must be a positive number") Long id, 
            @Valid @RequestBody StatusUpdateDto statusUpdate) {
        OperatorDto updatedOperator = operatorService.updateStatus(id, statusUpdate);
        return ResponseEntity.ok(updatedOperator);
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<TaskDto> createTask(
            @PathVariable @Positive(message = "Operator ID must be a positive number") Long id, 
            @Valid @RequestBody TaskDto taskDto) {
        TaskDto createdTask = operatorService.createTask(id, taskDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    @PostMapping("/bulk-import")
    public ResponseEntity<BulkImportResultDto> bulkImportOperators(
            @Valid @RequestBody BulkImportRequestDto bulkRequest) {
        
        if (bulkRequest.getOperators() != null && bulkRequest.getOperators().size() > 1000) {
            throw new IllegalArgumentException("Bulk import is limited to 1000 operators per request");
        }
        
        BulkImportResultDto result = operatorService.bulkImport(bulkRequest);
        return ResponseEntity.ok(result);
    }
}