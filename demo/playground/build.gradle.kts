plugins {
    kotlin("multiplatform")
}

repositories{
    jcenter()
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://dl.bintray.com/mipt-npm/dataforge")
    maven("https://dl.bintray.com/mipt-npm/kscience")
    maven("https://dl.bintray.com/mipt-npm/dev")
}

kotlin {

    js(IR) {
        browser {
            webpackTask {
                this.outputFileName = "js/visionforge-playground.js"
            }
        }
        binaries.executable()
    }

    jvm{
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    afterEvaluate {
        val jsBrowserDistribution by tasks.getting

        tasks.getByName<ProcessResources>("jvmProcessResources") {
            dependsOn(jsBrowserDistribution)
            afterEvaluate {
                from(jsBrowserDistribution)
            }
        }
    }


    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":visionforge-solid"))
                api(project(":visionforge-gdml"))
                api(project(":visionforge-plotly"))
            }
        }

        val jsMain by getting{
            dependencies {
                api(project(":ui:bootstrap"))
                api(project(":visionforge-threejs"))
            }
        }

        val jvmMain by getting{
            dependencies {
                api(project(":visionforge-server"))
                implementation("com.github.Ricky12Awesome:json-schema-serialization:0.6.6")
            }
        }
    }
}