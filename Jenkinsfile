node {
    try {
        def branchname = 'main'
        stage('Clone repository') {
            git([url: 'https://github.com/Sunbird-RC/sunbird-rc-core', branch: "${branchname}"])
        }

        stage('Compile And Test'){
            sh """sh configure-dependencies.sh"""
            sh """make"""
            step([$class: 'JacocoPublisher', exclusionPattern: '**/model/**,**/helpers/**,**/config/**,**/configs/**,**/controllers/**,**/models/**,**/exceptions/**,**/messaging/**,**/response/**,**/utils/**,**/migration/**,**/TestApplication.class,**/TestKafkaConfiguration.class,**/HealthCheck.class,**/TestSubmissionPipelineApplicationn.class,**/TestApplicationConfiguration.class,**/migration/**'])
        }

        stage('Build image') {
            app = docker.build("ghcr.io/sunbird-rc/sunbird-rc-core","target")
            claimApp = docker.build("ghcr.io/sunbird-rc/sunbird-rc-claim-ms","java/claim")
        }

        // stage('Test image') {
        //     app.withRun('-p 8010:8081') {c ->
        //         sh """#!/bin/bash
        //         env;
        //         i=0;
        //         while [[ \$i -lt 120 ]] ; do let i=i+1; sleep 1; status=`curl -I localhost:8010/health 2>/dev/null | grep 'HTTP/1.1 200' | wc -l`;if [ \$status -ge 1 ];then echo '\nTested Successfully';exit 0;else printf '.';  fi;done; exit 1;"""
        //     }
        // }


        stage('Push image') {
            docker.withRegistry('ghcr.io', 'sunbird-rc') {
                app.push("${env.BUILD_NUMBER}")
                app.push("latest")
           }
           docker.withRegistry('ghrc.io', 'sunbird-rc') {
               claimApp.push("${env.BUILD_NUMBER}")
               claimApp.push("latest")
          }
        }

//         stage('Deploy image') {
//             sh "ssh kesavan@10.4.0.6 'kubectl get pods -n ndear'"
//             sh "ssh kesavan@10.4.0.6 'kubectl set image deployment/registry registry=ghcr.io/sunbird-rc/sunbird-rc:${env.BUILD_NUMBER} --record --namespace=ndear'"
//             sh "ssh kesavan@10.4.0.6 'kubectl set image deployment/claim-ms claim-ms=ghcr.io/sunbird-rc/sunbird-rc-claim-ms:${env.BUILD_NUMBER} --record --namespace=ndear'"
//         }

    }
    catch (err) {
        currentBuild.result = "FAILURE"
        throw err
    }

}
