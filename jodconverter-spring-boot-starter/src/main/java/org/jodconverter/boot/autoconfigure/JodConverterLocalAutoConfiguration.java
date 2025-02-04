/*
 * Copyright 2004 - 2012 Mirko Nasato and contributors
 *           2016 - 2020 Simon Braconnier and contributors
 *
 * This file is part of JODConverter - Java OpenDocument Converter.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jodconverter.boot.autoconfigure;

import java.io.InputStream;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.document.DefaultDocumentFormatRegistryInstanceHolder;
import org.jodconverter.core.document.DocumentFormatRegistry;
import org.jodconverter.core.document.JsonDocumentFormatRegistry;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.core.util.StringUtils;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.jodconverter.local.office.LocalOfficeUtils;
import org.jodconverter.local.process.ProcessManager;

/** {@link EnableAutoConfiguration Auto-configuration} for JodConverter local module. */
@Configuration
@ConditionalOnClass(LocalConverter.class)
@ConditionalOnProperty(prefix = "jodconverter.local", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(JodConverterLocalProperties.class)
public class JodConverterLocalAutoConfiguration {

  private final JodConverterLocalProperties properties;

  /**
   * Creates the local auto configuration.
   *
   * @param properties The local properties.
   */
  public JodConverterLocalAutoConfiguration(final @NonNull JodConverterLocalProperties properties) {
    this.properties = properties;
  }

  // Creates the OfficeManager bean.
  private OfficeManager createOfficeManager(final ProcessManager processManager) {

    final LocalOfficeManager.Builder builder =
        LocalOfficeManager.builder()
            .officeHome(properties.getOfficeHome())
            .hostName(properties.getHostName())
            .portNumbers(properties.getPortNumbers())
            .workingDir(properties.getWorkingDir())
            .templateProfileDir(properties.getTemplateProfileDir())
            .existingProcessAction(properties.getExistingProcessAction())
            .processTimeout(properties.getProcessTimeout())
            .processRetryInterval(properties.getProcessRetryInterval())
            .afterStartProcessDelay(properties.getAfterStartProcessDelay())
            .disableOpengl(properties.isDisableOpengl())
            .startFailFast(properties.isStartFailFast())
            .keepAliveOnShutdown(properties.isKeepAliveOnShutdown())
            .taskQueueTimeout(properties.getTaskQueueTimeout())
            .taskExecutionTimeout(properties.getTaskExecutionTimeout())
            .maxTasksPerProcess(properties.getMaxTasksPerProcess());
    if (StringUtils.isBlank(properties.getProcessManagerClass())) {
      builder.processManager(processManager);
    } else {
      builder.processManager(properties.getProcessManagerClass());
    }

    // Starts the manager
    return builder.build();
  }

  /**
   * 进程管理器：负责匹配当前的系统类型，并负责执行当前系统的命令（如使用启动命令启动openOffice程序），调用RunTime类或ProcessBuilder类启动进程
   * @return
   */
  @Bean
  @ConditionalOnMissingBean(name = "processManager")
  /* default */ ProcessManager processManager() {
    return LocalOfficeUtils.findBestProcessManager();
  }

  /**
   * 文档格式注册器：负责控制OpenOffice支持转换的文件格式的集合
   * @param resourceLoader
   * @return
   * @throws Exception
   */
  @Bean
  @ConditionalOnMissingBean(name = "documentFormatRegistry")
  /* default */ DocumentFormatRegistry documentFormatRegistry(final ResourceLoader resourceLoader)
      throws Exception {

    try (InputStream in =
        // Load the json resource containing default document formats.
        StringUtils.isBlank(properties.getDocumentFormatRegistry())
            ? resourceLoader.getResource("classpath:document-formats.json").getInputStream()
            : resourceLoader.getResource(properties.getDocumentFormatRegistry()).getInputStream()) {

      // Create the registry
      final DocumentFormatRegistry registry =
          properties.getFormatOptions() == null
              ? JsonDocumentFormatRegistry.create(in)
              : JsonDocumentFormatRegistry.create(in, properties.getFormatOptions());

      // Set as default.
      DefaultDocumentFormatRegistryInstanceHolder.setInstance(registry);

      // Return it.
      return registry;
    }
  }

    /**
     * office运行启动的管理器：
     * 1、找到office的安装目录，工作目录
     * 2、构建启动office进程的命令
     * 3、调用processManager去启动office进程
     * @param processManager 由spring自动注入（因为添加了 @Bean 注解，创建带参数的bean时，会将相关的bean参数自动注入）
     * @return
     */
  @Bean(name = "localOfficeManager", initMethod = "start", destroyMethod = "stop")
  @ConditionalOnMissingBean(name = "localOfficeManager")
  /* default */ OfficeManager localOfficeManager(final ProcessManager processManager) {

    return createOfficeManager(processManager);
  }

  /**
   * 文档转换器：将office进程管理器和文件格式注册器进行组合，组成一个文档转换器
   * @param localOfficeManager
   * @param documentFormatRegistry
   * @return
   */
  // Must appear after the localOfficeManager bean creation. Do not reorder this class by name.
  @Bean
  @ConditionalOnMissingBean(name = "localDocumentConverter")
  @ConditionalOnBean(name = {"localOfficeManager", "documentFormatRegistry"})
  /* default */ DocumentConverter localDocumentConverter(
      final OfficeManager localOfficeManager, final DocumentFormatRegistry documentFormatRegistry) {

    return LocalConverter.builder()
        .officeManager(localOfficeManager)
        .formatRegistry(documentFormatRegistry)
        .build();
  }
}
