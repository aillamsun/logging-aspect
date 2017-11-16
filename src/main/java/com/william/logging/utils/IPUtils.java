package com.william.logging.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by Administrator on 2017/2/15.
 */
@Slf4j
public class IPUtils {

    /**
     * 获取域名 不包含端口
     *
     * @return
     */
    public static String getDomainNameExitPort(HttpServletRequest request) {
//获取域名
        StringBuffer url = request.getRequestURL();
        String domain_name = url.delete(url.length() - request.getRequestURI().length(), url.length()).append("/").toString();
        if (domain_name.contains(":")) {
            int i = domain_name.length() - domain_name.indexOf(":");
            domain_name = domain_name.substring(0, (i - 2));
        }
        String temp_domain_name = domain_name.substring(domain_name.length() - 1);
        if (!temp_domain_name.contains("/")) {
            domain_name = domain_name + "/";
        }
        return domain_name;
    }

    public static String getDomainNam(HttpServletRequest request) {
//获取域名
        String url = "http://"+request.getLocalAddr()+":"+request.getLocalPort();
        return url;
    }


    /**
     * 获取IP地址
     *
     * 使用Nginx等反向代理软件， 则不能通过request.getRemoteAddr()获取IP地址
     * 如果使用了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP地址，X-Forwarded-For中第一个非unknown的有效IP字符串，则为真实IP地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ip = null;
        try {
            ip = request.getHeader("x-forwarded-for");
            if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (StringUtils.isEmpty(ip) || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.error("IPUtils ERROR ", e);
        }

//        //使用代理，则获取第一个IP地址
//        if(StringUtils.isEmpty(ip) && ip.length() > 15) {
//			if(ip.indexOf(",") > 0) {
//				ip = ip.substring(0, ip.indexOf(","));
//			}
//		}

        return ip;
    }
}
