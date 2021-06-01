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
        }

    }
    catch (err) {
        currentBuild.result = "FAILURE"
        throw err
    }

}
