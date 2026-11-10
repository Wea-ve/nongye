package org.example.nongye.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CropBatch {
    private Long id;
    private String batchNo;
    private Long farmerId;
    private String cropName;
    private String variety;
    private String landLocation;
    private LocalDate plantDate;
    private LocalDate expectHarvestDate;
    private LocalDate actualHarvestDate;
    private BigDecimal harvestWeight;
    private String status;       // PLANTING/HARVESTED/SOLD
    private Integer onSale;
    private BigDecimal price;
    private BigDecimal stock;
    private Integer subscribeOpen;
    private String promiseSummary;
    private String traceCode;
    private String qrCodePath;
    private LocalDateTime createTime;

    // 联表字段
    private String farmerName;
    private String farmerPhone;
    private String farmerAddress;
}
