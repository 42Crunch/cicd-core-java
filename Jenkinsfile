node {
    stage('Checkout sources') {
        checkout scm
    }

    stage('Run tests') {
          sh './test.sh'
    }

    stage('Cleanup') {
        deleteDir()
    }
}
