def secrets = [
  [path: 'cbi/eclipse.pde/develocity.eclipse.org', secretValues: [
    [envVar: 'DEVELOCITY_ACCESS_KEY', vaultKey: 'api-token']
    ]
  ]
]

pipeline {
	options {
		timeout(time: 60, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
		timestamps()
	}
	agent {
		label 'ubuntu-latest'
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'temurin-jdk21-latest'
	}
	stages {
		stage('Build') {
			steps {
				xvnc(useXauthority: true) {
                    withVault([vaultSecrets: secrets]) {
                        sh """
                        mvn clean verify -Dmaven.repo.local=$WORKSPACE/.m2/repository \
                            --fail-at-end --update-snapshots --batch-mode --no-transfer-progress --show-version --errors \
                            -Pbree-libs -Papi-check -Pjavadoc -Ptck \
                            ${env.BRANCH_NAME=='master' ? '-Peclipse-sign': ''} \
                            -Dcompare-version-with-baselines.skip=false \
                            -Dmaven.test.failure.ignore=false \
                            -Dtycho.debug.artifactcomparator \
                            -Dpde.docs.baselinemode=fail
                        """
                    }
                }
			}
			post {
				always {
					archiveArtifacts(allowEmptyArchive: true, artifacts: '*.log,\
						*/target/work/data/.metadata/*.log,\
						*/tests/target/work/data/.metadata/*.log,\
						apiAnalyzer-workspace/.metadata/*.log,\
						**/target/artifactcomparison/**,\
						**/target/compilelogs/**,\
						repository/target/repository/**')
					discoverGitReferenceBuild referenceJob: 'eclipse.pde/master'
					junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
					recordIssues publishAllIssues: true, ignoreQualityGate: true, enabledForFailure: true, tools: [
							eclipse(name: 'Compiler', pattern: '**/target/compilelogs/*.xml'),
							issues(name: 'API Tools', id: 'apitools', pattern: '**/target/apianalysis/*.xml'),
							mavenConsole(),
							javaDoc()
						], qualityGates: [[threshold: 1, type: 'NEW', unstable: true]]
				}
			}
		}
		stage('Deploy') {
			when {
				branch 'master'
			}
			steps {
				sshagent(['projects-storage.eclipse.org-bot-ssh']) {
					 sh '''
						ssh genie.pde@projects-storage.eclipse.org "rm -rf /home/data/httpd/download.eclipse.org/pde/builds/master/*"
						scp -r repository/target/repository/* genie.pde@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/pde/builds/master/
						'''
				}
			}
		}
	}
}
