#!groovy

node('build-slave') {

    currentBuild.result = "SUCCESS"
    cleanWs()

    try {

        stage('Checkout') {
            checkout scm
        }

        stage('Build') {

            env.NODE_ENV = "build"
            print "Environment will be : ${env.NODE_ENV}"
            sh('git pull origin master')
            sh('pwd')
            sh ('sh configure-dependencies.sh')
	        sh('cd java && mvn clean install')
            sh('pwd')
	        sh('cd java/registry && mvn clean install')
            sh 'chmod 755 ./target/metadata.sh'
            sh('./build.sh')

        }

        stage('Publish') {

            echo 'Push to Repo'
            sh 'ls -al ~/'
            sh 'ARTIFACT_LABEL=bronze ./dockerPushToRepo.sh'
            sh './target/metadata.sh > metadata.json'
            sh 'cat metadata.json'
            archive includes: "metadata.json"

        }
    } catch (err) {

        currentBuild.result = "FAILURE"
        throw err

    }

}
