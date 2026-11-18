plugins {
    base
    id("aap.conventions") apply false
}

for (taskName in listOf("clean", "build", "assemble", "check")) {
    tasks.named(taskName) {
        dependsOn(subprojects.map { it.path + ":$taskName" })
    }
}
