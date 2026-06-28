package net.cannasmp.statsapi.services;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SystemMetricsService {
    public Map<String, Object> collect() {
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();
        Map<String, Object> ram = new LinkedHashMap<>();
        ram.put("usedBytes", runtime.totalMemory() - runtime.freeMemory());
        ram.put("freeBytes", runtime.freeMemory());
        ram.put("allocatedBytes", runtime.totalMemory());
        ram.put("maxBytes", runtime.maxMemory());

        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("heapUsedBytes", heap.getUsed());
        jvm.put("heapCommittedBytes", heap.getCommitted());
        jvm.put("heapMaxBytes", heap.getMax());
        jvm.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());

        Map<String, Object> cpu = new LinkedHashMap<>();
        try {
            com.sun.management.OperatingSystemMXBean os =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            cpu.put("processLoad", round(os.getProcessCpuLoad() * 100D));
            cpu.put("systemLoad", round(os.getCpuLoad() * 100D));
            cpu.put("cores", os.getAvailableProcessors());
        } catch (RuntimeException ex) {
            cpu.put("cores", Runtime.getRuntime().availableProcessors());
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cpu", cpu);
        map.put("ram", ram);
        map.put("jvm", jvm);
        return map;
    }

    private double round(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0D;
        }
        return Math.round(value * 100D) / 100D;
    }
}
