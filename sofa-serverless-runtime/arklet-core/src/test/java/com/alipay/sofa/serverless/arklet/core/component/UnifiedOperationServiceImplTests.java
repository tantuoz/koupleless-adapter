/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.serverless.arklet.core.component;

import com.alipay.sofa.ark.api.ArkClient;
import com.alipay.sofa.ark.api.ClientResponse;
import com.alipay.sofa.ark.spi.model.BizOperation;
import com.alipay.sofa.serverless.arklet.core.common.model.CombineInstallRequest;
import com.alipay.sofa.serverless.arklet.core.common.model.CombineInstallResponse;
import com.alipay.sofa.serverless.arklet.core.ops.CombineInstallHelper;
import com.alipay.sofa.serverless.arklet.core.ops.UnifiedOperationServiceImpl;
import com.google.common.base.Preconditions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

/**
 * @author mingmen
 * @date 2023/10/26
 */

@RunWith(MockitoJUnitRunner.class)
public class UnifiedOperationServiceImplTests {
    @InjectMocks
    private UnifiedOperationServiceImpl unifiedOperationService;

    @Spy
    private CombineInstallHelper combineInstallHelper;

    @Before
    public void setUp() {
        if (unifiedOperationService == null) {
            unifiedOperationService = new UnifiedOperationServiceImpl();
        }
    }

    /**
     * 测试初始化方法
     */
    @Test
    public void testInit() {
        unifiedOperationService.init();
    }

    /**
     * 测试销毁方法
     */
    @Test
    public void testDestroy() {
        unifiedOperationService.destroy();
    }

    /**
     * 测试安装方法，输入合法URL
     */
    @Test
    public void testInstallWithValidUrl() throws Throwable {
        try (MockedStatic<ArkClient> arkClientMockedStatic = Mockito.mockStatic(ArkClient.class)) {
            ClientResponse clientResponse = Mockito.mock(ClientResponse.class);
            arkClientMockedStatic.when(() -> ArkClient.installOperation(Mockito.any(BizOperation.class))).thenReturn(clientResponse);
            ClientResponse response = unifiedOperationService.install("http://example.com/biz.jar");
            arkClientMockedStatic.verify(() -> ArkClient.installOperation(Mockito.any(BizOperation.class)));
            Assert.assertEquals(clientResponse, response);
        }
    }

    /**
     * 测试卸载方法，输入合法的bizName和bizVersion
     */
    @Test
    public void testUninstallWithValidBizNameAndVersion() throws Throwable {
        try (MockedStatic<ArkClient> arkClientMockedStatic = Mockito.mockStatic(ArkClient.class)) {
            ClientResponse clientResponse = Mockito.mock(ClientResponse.class);
            arkClientMockedStatic.when(() -> ArkClient.uninstallBiz(Mockito.anyString(), Mockito.anyString())).thenReturn(clientResponse);
            ClientResponse response = unifiedOperationService.uninstall("bizName", "1.0.0");
            arkClientMockedStatic.verify(() -> ArkClient.uninstallBiz(Mockito.anyString(), Mockito.anyString()));
            Assert.assertEquals(clientResponse, response);
        }
    }

    /**
     * 测试切换Biz方法，输入合法的bizName和bizVersion
     */
    @Test
    public void testSwitchBizWithValidBizNameAndVersion() throws Throwable {
        try (MockedStatic<ArkClient> arkClientMockedStatic = Mockito.mockStatic(ArkClient.class)) {
            ClientResponse clientResponse = Mockito.mock(ClientResponse.class);
            arkClientMockedStatic.when(() -> ArkClient.switchBiz(Mockito.anyString(), Mockito.anyString())).thenReturn(clientResponse);
            ClientResponse response = unifiedOperationService.switchBiz("bizName", "1.0.0");
            arkClientMockedStatic.verify(() -> ArkClient.switchBiz(Mockito.anyString(), Mockito.anyString()));
            Assert.assertEquals(clientResponse, response);
        }
    }

    @Test
    public void testCombineInstall() {
        MockedStatic<Files> filesMockedStatic = null;
        // there are limitations to this unit test
        // more specifically, static mock cannot go out of current thread.
        // however, the method would new a thread to do the job.
        // thus ArkClient.installOperation would not be mocked.
        try {
            BasicFileAttributes fileAttr = Mockito.mock(BasicFileAttributes.class);
            Path path0 = Mockito.mock(Path.class);
            doReturn("/file/a-biz.jar").when(path0).toString();
            Path path1 = Mockito.mock(Path.class);
            doReturn("/file/b-biz.jar").when(path1).toString();
            Path path_omit = Mockito.mock(Path.class);
            doReturn("/file/notbiz.jar").when(path_omit).toString();

            Path path = Mockito.mock(Path.class);
            doReturn(path0, path1, path_omit).when(path).toAbsolutePath();

            filesMockedStatic = Mockito.mockStatic(Files.class);
            filesMockedStatic.when(() -> Files.walkFileTree(Mockito.any(), Mockito.any())).thenAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                    FileVisitor<Path> argument0 = invocationOnMock.getArgument(1, FileVisitor.class);
                    argument0.visitFile(path, fileAttr);
                    argument0.visitFile(path, fileAttr);
                    argument0.visitFile(path, fileAttr);
                    return null;
                }
            });

            doReturn(new HashMap<>()).when(combineInstallHelper).getMainAttributes(anyString());

            CombineInstallResponse response = unifiedOperationService.combineInstall(CombineInstallRequest.builder().
                    bizDirAbsolutePath("/path/to/biz").
                    build());

            Assert.assertTrue(response.getBizUrlToResponse().containsKey("/file/a-biz.jar"));
            Assert.assertTrue(response.getBizUrlToResponse().containsKey("/file/b-biz.jar"));
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        } finally {
            if (filesMockedStatic != null) {
                filesMockedStatic.close();
            }
        }
    }
}
