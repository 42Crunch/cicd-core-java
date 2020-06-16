node {
    stage('Checkout sources') {
        checkout scm
    }

    stage('Run tests') {
        withCredentials([string(credentialsId: 'b81db935-7187-45b4-b63b-6120e0f05d43', variable: 'TEST_API_KEY')]) {
            sh './test.sh'
        }
    }

    stage('Cleanup') {
        deleteDir()
    }

    post {
        success {  
            slackSend(color: '#FF0000', message: "@anton ${env.JOB_NAME} successfully completed")
        }
    }

}
