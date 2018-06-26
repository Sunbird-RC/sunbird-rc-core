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
	    sh('cd java')
            sh 'mvn clean install'
	    sh('cd registry')
            sh 'mvn clean install'
	    sh('cd ../../')
            sh('chmod 777 ./build.sh')
            sh('./build.sh')

        }

        stage('Publish') {

            echo 'Push to Repo'
            sh 'ls -al ~/'
            sh('chmod 777 ./dockerPushToRepo.sh')
            sh 'ARTIFACT_LABEL=bronze ./dockerPushToRepo.sh'
            sh './metadata.sh > metadata.json'
            sh 'cat metadata.json'
            archive includes: "metadata.json"

        }
    } catch (err) {

        currentBuild.result = "FAILURE"
        throw err

    }

}
