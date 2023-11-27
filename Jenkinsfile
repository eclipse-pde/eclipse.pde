def deployBranch = 'master'

pipeline {
	options {
		timeout(time: 40, unit: 'MINUTES')
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
						-Dmaven.test.error.ignore=true -Dmaven.test.failure.ignore=true
					'''
				}
			}
			post {
				always {
					archiveArtifacts(allowEmptyArchive: true, artifacts: '*.log, \
						*/target/work/data/.metadata/*.log, \
						*/tests/target/work/data/.metadata/*.log, \
						apiAnalyzer-workspace/.metadata/*.log')
					junit '**/target/surefire-reports/*.xml'
					discoverGitReferenceBuild referenceJob: 'eclipse.pde/master'
					recordIssues publishAllIssues: true, tools: [java(), mavenConsole(), javaDoc()]
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
