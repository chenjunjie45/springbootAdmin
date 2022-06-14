package com.example.springboot_demo;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent;
import de.codecentric.boot.admin.server.notify.AbstractStatusChangeNotifier;
import reactor.core.publisher.Mono;

public class DingDingNotifier extends AbstractStatusChangeNotifier {
    private static final String template = "<<<%s>>> \n 【服务名】: %s(%s) \n 【状态】: %s(%s) \n 【服务ip】: %s \n 【详情】: %s";


    private String[] ignoreChanges = new String[]{"UNKNOWN:UP","DOWN:UP","OFFLINE:UP"};

    public DingDingNotifier(InstanceRepository repository) {
        super(repository);
    }
    @Override
    protected boolean shouldNotify(InstanceEvent event, Instance instance) {
        if (!(event instanceof InstanceStatusChangedEvent)) {
            return false;
        } else {
            InstanceStatusChangedEvent statusChange = (InstanceStatusChangedEvent)event;
            String from = this.getLastStatus(event.getInstance());
            String to = statusChange.getStatusInfo().getStatus();

            if (from.equals(to)){
                return false;
            }
            if (to.equals("UNKNOWN") || to.equals("DOWN") || to.equals("OFFLINE") || to.equals("UP")) {
                return true;
            }
            else{
                return false;
            }

//            return Arrays.binarySearch(this.ignoreChanges, from + ":" + to) < 0 && Arrays.binarySearch(this.ignoreChanges, "*:" + to) < 0 && Arrays.binarySearch(this.ignoreChanges, from + ":*") < 0;

        }
    }


    @Override
    protected Mono<Void> doNotify(InstanceEvent event, Instance instance) {
        String serviceName = instance.getRegistration().getName();
        String serviceUrl = instance.getRegistration().getServiceUrl();
        String status = instance.getStatusInfo().getStatus();
        Map<String, Object> details = instance.getStatusInfo().getDetails();
        String instanceId = String.valueOf(instance.getId());
        StringBuilder str = new StringBuilder();
        String message = "";
//        if (status=="DOWN") {
//            message = "(服务健康检查失败)";
//        }
//        else if(status=="UP"){
//            message =  "(服务上线)";
//
//        }
//        else {
//            message =  "(服务离线)";
//        }


        switch (status) {
            // 健康检查没通过
            case "DOWN":
                message = "(服务健康检查失败)";
                break;
            // 服务离线
            case "OFFLINE":
                message =  "(服务离线)";
                break;
            //服务上线
            case "UP":
                message =  "(服务上线)";
                break;
            // 服务未知异常
            case "UNKNOWN":
                message =  "(未知异常)";


                break;
            default:
                break;
        }
        Date date = new Date();
        SimpleDateFormat ft2 = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
        str.append("服务名:" + serviceName + "("+instanceId+")\n");
        str.append("状态:" + status + message + "\n");
        str.append("服务ip:" + serviceUrl + "\n");
        str.append("触发时间:" + ft2.format(date) + "\n");
        return Mono.fromRunnable(() -> {
            DingDingMessageUtil.sendTextMessage(str.toString());
        });
    }
}

