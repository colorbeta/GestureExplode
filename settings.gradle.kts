pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // --- 加上这两行阿里云镜像，直接绕过代理毒缓存 ---
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        // ↓↓↓ 就是下面这行，告诉系统去 GitHub 开源仓库找拼音库 ↓↓↓
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Gesture Explode"
include(":app")
