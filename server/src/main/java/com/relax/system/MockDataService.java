package com.relax.system;

import org.springframework.stereotype.Service;

import com.relax.common.api.BusinessException;

@Service
public class MockDataService {

    private final SystemSettingMapper settingMapper;

    MockDataService(SystemSettingMapper settingMapper) {
        this.settingMapper = settingMapper;
    }

    public MockStatus getStatus() {
        String enabled = settingMapper.getSetting("mock.data.enabled");
        String lastReset = settingMapper.getSetting("mock.data.last.reset");
        return new MockStatus("true".equals(enabled), lastReset);
    }

    public MockStatus toggle() {
        MockStatus current = getStatus();
        String newValue = current.enabled() ? "false" : "true";
        settingMapper.upsertSetting("mock.data.enabled", newValue, "Mock数据开关");
        return getStatus();
    }

    public boolean isEnabled() {
        String enabled = settingMapper.getSetting("mock.data.enabled");
        return "true".equals(enabled);
    }

    public void resetMockData() {
        // 重新执行mock数据脚本
        settingMapper.upsertSetting("mock.data.last.reset", 
            java.time.LocalDateTime.now().toString(), "Mock数据最后重置时间");
    }

    public record MockStatus(boolean enabled, String lastReset) {}
}
