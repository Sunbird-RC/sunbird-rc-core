node {
    try {
        def branchname = 'open-saber-rc-2'
        stage('Clone repository') {
            git([url: 'https://github.com/tejash-jl/open-saber', branch: "${branchname}"])
        }

        stage('Compile And Test'){
            dir('java') {
              sh """./mvnw clean install -DskipTests"""
            }
            sh """rm -rf target"""
            sh """mkdir target"""
            dir('target') {
              sh """jar -xvf ../java/registry/target/registry.jar"""
            }
        }

        stage('Build image') {
            app = docker.build("tejashjl/open-saber-rc",".")
        }

        stage('Push image') {
            docker.withRegistry('https://registry.hub.docker.com', 'dockerhub') {
                app.push("${env.BUILD_NUMBER}")
                app.push("latest")
           }
        }

        stage('Deploy image') {
            sh "ssh dileep@40.80.94.137 'kubectl get pods -n ndear'"
            sh "ssh dileep@40.80.94.137 'kubectl set image deployment/registry registry=tejashjl/open-saber-rc:${env.BUILD_NUMBER} --record --namespace=ndear'"

        }

    }
    catch (err) {
        currentBuild.result = "FAILURE"
        throw err
    }

}
