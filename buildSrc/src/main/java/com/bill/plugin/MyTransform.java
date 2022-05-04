package com.bill.plugin;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * Created by Bill on 2022/5/3.
 */

class MyTransform extends Transform {

    // transform的名称
    // transformClassesWith + getName() + For + Debug或Release
    @Override
    public String getName() {
        return "MyTrans";
    }

    // 需要处理的数据类型，有两种枚举类型
    // CLASSES和RESOURCES，CLASSES代表处理的java的class文件，RESOURCES代表要处理java的资源
    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    // 指Transform要操作内容的范围
    // SCOPE_FULL_PROJECT 指当前项目下所以Module
    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    // 指明当前Transform是否支持增量编译
    @Override
    public boolean isIncremental() {
        return false;
    }

    // Transform中的核心方法，
    // inputs中是传过来的输入流，其中有两种格式，一种是jar包格式一种是目录格式。
    // outputProvider 获取到输出目录，最后将修改的文件复制到输出目录，这一步必须做不然编译会报错
    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        System.out.println("Hello MyTransform...");

        Collection<TransformInput> inputs = transformInvocation.getInputs();
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();

        if (!transformInvocation.isIncremental()) {
            // 非增量编译，则删除之前的所有输出
            outputProvider.deleteAll();
        }

        for (TransformInput input : inputs) {
            // 处理输入的目录
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                File dest = outputProvider.getContentLocation(directoryInput.getName(), directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
                // TODO 处理class文件
                FileUtils.copyDirectory(directoryInput.getFile(), dest); // 拷贝目录
            }

            // 处理输入的Jar包
            for (JarInput jarInput : input.getJarInputs()) {
                File dest = outputProvider.getContentLocation(jarInput.getName(), jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                // TODO 处理class文件
                FileUtils.copyFile(jarInput.getFile(), dest); // 拷贝Jar文件
            }
        }
    }

}
