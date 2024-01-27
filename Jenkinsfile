def deployBranch = 'master'

pipeline {
	options {
		timeout(time: 60, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
		timestamps()
	}
	agent {
		label 'centos-latest-8gb'
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'temurin-jdk17-latest'
	}
	environment {
		MVN_GOALS = getMavenGoals()
	}
	stages {
		stage('Build') {
			steps {
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh '''
						mvn clean ${MVN_GOALS} --batch-mode -Dmaven.repo.local=$WORKSPACE/.m2/repository \
						-Pbree-libs \
						-Papi-check \
						-Pjavadoc \
						-Dmaven.test.error.ignore=true -Dmaven.test.failure.ignore=true \
						-Dtycho.debug.artifactcomparator \
						-Dpde.docs.baselinemode=fail
					'''
				}
			}
			post {
				always {
					archiveArtifacts(allowEmptyArchive: true, artifacts: '*.log,\
						*/target/work/data/.metadata/*.log,\
						*/tests/target/work/data/.metadata/*.log,\
						apiAnalyzer-workspace/.metadata/*.log,\
						*/target/artifactcomparison/**,\
						repository/target/repository/**')
					junit '**/target/surefire-reports/*.xml'
					discoverGitReferenceBuild referenceJob: 'eclipse.pde/master'
					recordIssues publishAllIssues: true, tools: [eclipse(pattern: '**/target/compilelogs/*.xml'), mavenConsole(), javaDoc()], qualityGates: [[threshold: 1, type: 'NEW', unstable: true]]
				}
			}
		}
	}
}

def getMavenGoals() {
	//if(env.BRANCH_NAME == deployBranch) {
		return "deploy -DdeployAtEnd=true"
	//}
	//	return "verify"
}
