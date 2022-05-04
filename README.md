# Android Transform 使用

#### 什么是 Transform

Transform API 可以让我们在编译打包安卓项目时，在源码编译为 class 字节码后，处理成 dex 文件前，对字节码做一些操作。

#### 使用 Transform

本例子使用 buildSrc 的方式使用 Transform

##### 1、新建 Android 项目，并创建 buildSrc 插件，并配置好插件，其中 build.gradle 内容如下：

```
apply plugin: 'java-library'

repositories {
    google()
    mavenCentral()
}

dependencies {
    // 插件需要的API在这里
    implementation 'com.android.tools.build:gradle:4.0.0'

}

sourceSets {
    main {
        java{
            srcDir 'src/main/java'
        }
        resources {
            srcDir 'src/main/resources'
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

```

##### 2、插件代码如下：

```
package com.bill.plugin;

import com.android.build.gradle.AppExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

class MyPlugin implements Plugin<Project> {

    @Override
    public void apply(Project target) {

        AppExtension extension = target.getExtensions().findByType(AppExtension.class);
        if (extension != null) {
            extension.registerTransform(new MyTransform());
        }

    }
}

```

##### 3、创建 Transform 类，这里名为 MyTransform

```
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

    // Transform中的核心方法
    // inputs中是传过来的输入流，其中有两种格式，一种是jar包格式一种是目录格式。
    // outputProvider 获取到输出目录，最后将修改的文件复制到输出目录，这一步必须做不然编译会报错
    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        System.out.println("Hello MyTransform...");
    }

}

```

##### 注意：编写 Transform 后，重写 transform 打印日志，然后 Make Project 就可以在 Build 里看到输出里，但是输出的 apk 有问题，apk 内没内容里，所以需要通过下面代码拷贝 class ：

```
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
```

##### 通过上面的步骤就编写完成，这里只是打印个日志看配置，然后使用插件，在 app 中引入插件如下：

```
apply plugin: 'com.bill.plugin'
```

##### 点击 AS 的 Make Project ，然后在 Build Output 下可以看到输出：

```
> Task :app:transformClassesWithMyTransForDebug
Hello MyTransform...
```