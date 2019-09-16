import java.nio.file.Paths
import java.nio.file.Files

def projectPath = Paths.get(request.outputDirectory, request.artifactId)

["mvnw", "mvnw.cmd"].each {
    def mavenWrapperFile = projectPath.resolve(it).toFile()
    mavenWrapperFile.setExecutable(true, false)
}

/******* IMPORTANT
 * Everything below is an ugly workaround for most probably a bug in
 * maven archetype plugin which does not copy some files (Makefile, mvnw in our case)
 * when java files are present in `src/it/java` directory
 */
def choppedPackageName = request.package.tokenize('.') // eg. [uk, gov, caz]

def integrationTestsBasePath = projectPath.resolve("src").resolve("it")
def integrationTestsJavaPath = integrationTestsBasePath.resolve("java")

def packageDestPath = choppedPackageName.inject(integrationTestsJavaPath) {
    tempPath,
    item -> tempPath.resolve(item)
}
// creates the package directory, e.g. `src/it/uk/gov/caz'
packageDestPath = Files.createDirectories(packageDestPath)

def toBeMovedBasePath = integrationTestsBasePath.resolve("resources").resolve("to-be-moved")

// creates 'annotation' directory for 'IntegrationTest.java'
def annotationDestPath = packageDestPath.resolve("annotation")
Files.createDirectory(annotationDestPath)

// moves 'IntegrationTest.java' annotation to the required package
def annotationSourcePath = toBeMovedBasePath.resolve("IntegrationTest.java")
Files.move(annotationSourcePath, annotationDestPath.resolve(annotationSourcePath.getFileName()))

// moves sample integration test ('ApplicationTestIT.java') to a IT directory
def testSourcePath = toBeMovedBasePath.resolve("ApplicationTestIT.java")
Files.move(testSourcePath, packageDestPath.resolve(testSourcePath.getFileName()))

Files.delete(toBeMovedBasePath.resolve("README.md"))
Files.delete(toBeMovedBasePath)