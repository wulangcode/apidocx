package io.apidocx.process.yapi;

import static io.apidocx.base.util.NotificationUtils.notifyError;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import io.apidocx.action.AbstractAction;
import io.apidocx.base.sdk.yapi.YapiClient;
import io.apidocx.base.sdk.yapi.model.ApiInterface;
import io.apidocx.base.sdk.yapi.model.TestResult.Code;
import io.apidocx.config.ApidocxConfig;
import io.apidocx.model.Api;
import io.apidocx.process.yapi.config.YapiSettings;
import io.apidocx.process.yapi.config.YapiSettingsDialog;
import io.apidocx.process.yapi.process.YapiUploader;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 处理Yapi上传入口动作.
 */
public class YapiUploadAction extends AbstractAction {

    public static final String ACTION_TEXT = "Upload To YApi";

    @Override
    public boolean before(AnActionEvent event, ApidocxConfig config) {
        String projectId = config.getYapiProjectId();
        if (StringUtils.isEmpty(projectId)) {
            notifyError("Config file error", "yapiProjectId must not be empty.");
            return false;
        }
        if (StringUtils.isNotEmpty(config.getYapiProjectToken()) && StringUtils.isEmpty(config.getYapiUrl())) {
            notifyError("Config file error", "yapiUrl must not be empty, when you config yapiProjectToken.");
            return false;
        }
        if (StringUtils.isNotEmpty(config.getYapiProjectToken())) {
            return true;
        }

        Project project = event.getData(CommonDataKeys.PROJECT);
        YapiSettings settings = YapiSettings.getInstance();
        if (!settings.isValidate() || Code.OK != settings.testSettings().getCode()) {
            YapiSettingsDialog dialog = YapiSettingsDialog.show(project, event.getPresentation().getText());
            return !dialog.isCanceled();
        }
        return true;
    }

    @Override
    public void handle(AnActionEvent event, ApidocxConfig config, List<Api> apis) {
        Integer projectId = Integer.valueOf(config.getYapiProjectId());
        Project project = event.getData(CommonDataKeys.PROJECT);

        YapiSettings settings = YapiSettings.getInstance();
        YapiClient client = createClient(config, settings);
        YapiUploader uploader = new YapiUploader(client);

        super.handleUploadAsync(project, apis,
                api -> {
                    ApiInterface yapi = uploader.upload(projectId, api);

                    ApiUploadResult result = new ApiUploadResult();
                    result.setCategoryUrl(client.calculateCatUrl(projectId, yapi.getCatid()));
                    if (yapi.getId() != null) {
                        result.setApiUrl(client.calculateInterfaceUrl(projectId, yapi.getId()));
                    } else {
                        result.setApiUrl(result.getCategoryUrl());
                    }
                    return result;
                }, null);
    }

    private YapiClient createClient(ApidocxConfig config, YapiSettings settings) {
        if (StringUtils.isNotEmpty(config.getYapiProjectToken())) {
            return new YapiClient(config.getYapiUrl(), config.getYapiProjectToken());
        }
        return new YapiClient(settings.getUrl(), settings.getAccount(), settings.getPassword(),
                settings.getLoginWay(), settings.getCookies());
    }


    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setText(ACTION_TEXT);
    }
}