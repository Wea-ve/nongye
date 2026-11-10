package org.example.nongye.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.nongye.entity.Complaint;
import org.example.nongye.entity.CropBatch;
import org.example.nongye.entity.SysUser;
import org.example.nongye.mapper.ComplaintMapper;
import org.example.nongye.mapper.CropBatchMapper;
import org.example.nongye.util.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理端 Excel 导出（前端 SPA 直接以链接方式下载）
 * 仅保留导出能力，其余旧 Thymeleaf 页面已下线并统一跳转 SPA。
 */
@Controller
@RequestMapping("/admin")
public class AdminExportController {

    @Autowired
    private CropBatchMapper batchMapper;
    @Autowired
    private ComplaintMapper complaintMapper;

    /** 导出种植批次 */
    @GetMapping("/batches/export")
    public void exportBatches(HttpSession session, HttpServletResponse response) throws Exception {
        checkAdmin(session);
        List<CropBatch> batches = batchMapper.selectAll();
        String[] headers = {"批次号", "作物", "品种", "产地", "农户", "播种日期", "采收日期", "采收重量(斤)", "状态"};
        List<List<Object>> rows = new ArrayList<>();
        for (CropBatch b : batches) {
            List<Object> row = new ArrayList<>();
            row.add(b.getBatchNo());
            row.add(b.getCropName());
            row.add(b.getVariety());
            row.add(b.getLandLocation());
            row.add(b.getFarmerName());
            row.add(b.getPlantDate());
            row.add(b.getActualHarvestDate());
            row.add(b.getHarvestWeight());
            row.add("PLANTING".equals(b.getStatus()) ? "种植中" :
                    ("HARVESTED".equals(b.getStatus()) ? "已采收" : "已售罄"));
            rows.add(row);
        }
        writeExcel(response, "种植批次", headers, rows, "batches.xlsx");
    }

    /** 导出投诉工单 */
    @GetMapping("/complaints/export")
    public void exportComplaints(HttpSession session, HttpServletResponse response) throws Exception {
        checkAdmin(session);
        List<Complaint> complaints = complaintMapper.selectAll();
        String[] headers = {"ID", "投诉人", "电话", "类型", "内容", "关联批次", "作物", "状态", "回复", "创建时间"};
        List<List<Object>> rows = new ArrayList<>();
        for (Complaint c : complaints) {
            List<Object> row = new ArrayList<>();
            row.add(c.getId());
            row.add(c.getComplainant());
            row.add(c.getPhone());
            row.add(c.getType());
            row.add(c.getContent());
            row.add(c.getBatchNo());
            row.add(c.getCropName());
            row.add("PENDING".equals(c.getStatus()) ? "待处理" :
                    ("PROCESSING".equals(c.getStatus()) ? "处理中" : "已关闭"));
            row.add(c.getReply());
            row.add(c.getCreateTime());
            rows.add(row);
        }
        writeExcel(response, "投诉工单", headers, rows, "complaints.xlsx");
    }

    private void checkAdmin(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("loginUser");
        if (user == null) throw new RuntimeException("未登录");
        if (!"admin".equals(user.getRole())) throw new RuntimeException("无权限");
    }

    private void writeExcel(HttpServletResponse response, String sheetName, String[] headers,
                            List<List<Object>> rows, String fileName) throws Exception {
        byte[] data = ExcelUtil.export(sheetName, headers, rows);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" +
                URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }
}
