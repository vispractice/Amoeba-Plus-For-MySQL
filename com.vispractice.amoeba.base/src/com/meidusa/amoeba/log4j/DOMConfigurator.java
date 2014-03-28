package com.meidusa.amoeba.log4j;

import org.apache.log4j.LogManager;
import org.apache.log4j.helpers.FileWatchdog;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.OptionConverter;

import org.w3c.dom.Element;

import java.io.InputStream;
import java.net.URL;

import java.util.Properties;

/**
 * 从XML文件配置log4j的工具类。和Log4j默认的<code>DOMConfigurator</code>相比，这个类允许提供额外的properties对象，在配置文件中被引用。
 */
public class DOMConfigurator extends org.apache.log4j.xml.DOMConfigurator {
    private Properties props;

    /**
     * 创建新对象。
     */
    public DOMConfigurator() {
        this(null);
    }

    /**
     * 创建新对象。
     *
     * @param props 可在配置文件中被引用的属性
     */
    public DOMConfigurator(Properties props) {
        this.props = props;
    }

    /**
     * 使用XML文件配置log4j。
     *
     * @param filename 配置文件名
     */
    public static void configure(String filename) {
        new DOMConfigurator().doConfigure(filename, LogManager.getLoggerRepository());
    }

    /**
     * 使用XML文件配置log4j。
     *
     * @param stream 配置文件输入流
     */
    public static void configure(InputStream stream) {
        new DOMConfigurator().doConfigure(stream, LogManager.getLoggerRepository());
    }
    
    /**
     * 使用XML文件配置log4j。
     *
     * @param filename 配置文件名
     * @param props 可在配置文件中被引用的属性
     */
    public static void configure(String filename, Properties props) {
        new DOMConfigurator(props).doConfigure(filename, LogManager.getLoggerRepository());
    }

    /**
     * 使用XML文件配置log4j。
     *
     * @param url 配置文件的URL
     */
    public static void configure(URL url) {
        new DOMConfigurator().doConfigure(url, LogManager.getLoggerRepository());
    }

    /**
     * 使用XML文件配置log4j。
     *
     * @param url 配置文件的URL
     * @param props 可在配置文件中被引用的属性
     */
    public static void configure(URL url, Properties props) {
        new DOMConfigurator(props).doConfigure(url, LogManager.getLoggerRepository());
    }

    /**
     * 使用XML文件配置log4j。
     *
     * @param element 配置文件名的DOM element
     */
    public static void configure(Element element) {
        new DOMConfigurator().doConfigure(element, LogManager.getLoggerRepository());
    }

    /**
     * 使用XML文件配置log4j。
     *
     * @param element 配置文件名的DOM element
     * @param props 可在配置文件中被引用的属性
     */
    public static void configure(Element element, Properties props) {
        new DOMConfigurator(props).doConfigure(element, LogManager.getLoggerRepository());
    }

    /**
     * 使用XML文件配置log4j，同时监控文件的更改。
     *
     * @param filename 配置文件名
     */
    public static void configureAndWatch(String filename) {
        configureAndWatch(filename, null, FileWatchdog.DEFAULT_DELAY);
    }

    /**
     * 使用XML文件配置log4j，同时监控文件的更改。
     *
     * @param filename 配置文件名
     * @param props 可在配置文件中被引用的属性
     */
    public static void configureAndWatch(String filename, Properties props) {
        configureAndWatch(filename, props, FileWatchdog.DEFAULT_DELAY);
    }

    /**
     * 使用XML文件配置log4j，同时监控文件的更改。此方法将创建一个监控线程，该线程第隔指定时间就会检查文件是否被创建或改变，如果是，则从文件中读取log4j配置。
     *
     * @param filename 配置文件名
     * @param interval 监控线程检查间隔（ms）
     */
    public static void configureAndWatch(String filename, long interval) {
        XMLWatchdog xdog = new XMLWatchdog(filename, null);

        xdog.setDelay(interval);
        xdog.start();
    }

    /**
     * 使用XML文件配置log4j，同时监控文件的更改。此方法将创建一个监控线程，该线程第隔指定时间就会检查文件是否被创建或改变，如果是，则从文件中读取log4j配置。
     *
     * @param filename 配置文件名
     * @param props 可在配置文件中被引用的属性
     * @param interval 监控线程检查间隔（ms）
     */
    public static void configureAndWatch(String filename, Properties props, long interval) {
        XMLWatchdog xdog = new XMLWatchdog(filename, props);

        xdog.setDelay(interval);
        xdog.start();
    }

    /**
     * 设置属性，这些属性可以在配置文件中被引用。
     *
     * @param props 属性
     */
    public void setProperties(Properties props) {
        this.props = props;
    }

    /**
     * 替换字符串值，将其中的${xxx}替换成具体的值。
     *
     * @param value 要替换的值
     *
     * @return 替换后的值
     */
    protected String subst(String value) {
        try {
            return OptionConverter.substVars(value, props);
        } catch (IllegalArgumentException e) {
            LogLog.warn("Could not perform variable substitution.", e);
            return value;
        }
    }

    /**
     * 监控线程。
     */
    private static class XMLWatchdog extends FileWatchdog {
        private Properties props;

        public XMLWatchdog(String filename, Properties props) {
            super(filename);
            this.props = props;
        }

        public void doOnChange() {
            new DOMConfigurator(props).doConfigure(filename, LogManager.getLoggerRepository());
            LogLog.warn("log4j config load completed from file:"+filename);
        }
    }
}
