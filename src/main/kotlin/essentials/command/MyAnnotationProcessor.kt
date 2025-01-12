package essentials.command

@AutoService(Processor::class)
class MyAnnotationProcessor : AbstractProcessor() {
    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(MyAnnotation::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        for (element in roundEnv.getElementsAnnotatedWith(MyAnnotation::class.java)) {
            if (element.kind != ElementKind.METHOD) {
                continue
            }

            val annotation = element.getAnnotation(MyAnnotation::class.java)
            val methodName = element.simpleName.toString()
            val className = (element.enclosingElement as TypeElement).qualifiedName.toString()

            // 여기에 코드를 생성하거나 변경하는 로직을 추가할 수 있습니다.
            println("Found method: $methodName in class: $className with value: ${annotation.value}")
        }
        return true
    }
}
