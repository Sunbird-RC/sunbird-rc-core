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
            app = docker.build("open-saber-rc",".")
        }

        stage('Deploy image') {
            sh "ssh dileep@40.80.94.137 'kubectl get pods -n ndear'"
        }

    }
    catch (err) {
        currentBuild.result = "FAILURE"
        throw err
    }

}
