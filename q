[1mdiff --git a/.drone.yml b/.drone.yml[m
[1mindex 978f0cd..3f2867c 100644[m
[1m--- a/.drone.yml[m
[1m+++ b/.drone.yml[m
[36m@@ -1,3 +1,66 @@[m
[32m+[m[32m  #################################################### Nightly Steps ############################################################[m
[32m+[m[32mkind: pipeline[m
[32m+[m[32mtype: docker[m
[32m+[m[32mname: payments-api-nightly[m
[32m+[m[32mtrigger:[m
[32m+[m[32m  event:[m
[32m+[m[32m    - cron[m
[32m+[m[32mconcurrency:[m
[32m+[m[32m  limit: 1[m
[32m+[m[32mvolumes:[m
[32m+[m[32m- name: docker_sock[m
[32m+[m[32m  host:[m
[32m+[m[32m    path: /var/run/docker.sock[m
[32m+[m
[32m+[m[32msteps:[m
[32m+[m
[32m+[m[32m  # Build Docker Image for Running Maven[m
[32m+[m[32m  - name: build docker maven base[m
[32m+[m[32m    image: docker[m
[32m+[m[32m    commands:[m
[32m+[m[32m      - docker build -t vccs-api-base -f ci-cd-resources/Dockerfile.ci.base .[m
[32m+[m[32m    volumes:[m
[32m+[m[32m      - name: docker_sock[m
[32m+[m[32m        path: /var/run/docker.sock[m
[32m+[m
[32m+[m[32m  - name: owasp dependency scan[m
[32m+[m[32m    image: payments-api-base[m
[32m+[m[32m    pull: never[m
[32m+[m[32m    commands:[m
[32m+[m[32m      - mvn org.owasp:dependency-check-maven:check -P security[m
[32m+[m[32m    environment:[m
[32m+[m[32m      JAQU_NEXUS_URL:[m
[32m+[m[32m        from_secret: nexus_url[m
[32m+[m[32m      JAQU_NEXUS_USER:[m
[32m+[m[32m        from_secret: nexus_username[m
[32m+[m[32m      JAQU_NEXUS_PASSWORD:[m
[32m+[m[32m        from_secret: nexus_password[m
[32m+[m
[32m+[m
[32m+[m[32m# Build end to end tests docker image[m
[32m+[m[32m  - name: build end to end test image[m
[32m+[m[32m    image: docker:git[m
[32m+[m[32m    commands:[m
[32m+[m[32m      - mkdir ~/.ssh[m
[32m+[m[32m      - echo -n "$GIT_PRIVATE_SSH" > ~/.ssh/id_rsa[m
[32m+[m[32m      - chmod 600 ~/.ssh/id_rsa[m
[32m+[m[32m      - touch ~/.ssh/known_hosts[m
[32m+[m[32m      - chmod 600 ~/.ssh/known_hosts[m
[32m+[m[32m      - ssh-keyscan -H github.com > /etc/ssh/ssh_known_hosts 2> /dev/null[m
[32m+[m[32m      - git clone git@github.com:InformedSolutions/JAQU-CAZ-QA-selenium.git[m
[32m+[m[32m      - cd JAQU-CAZ-QA-selenium[m
[32m+[m[32m      - docker build -t selenium-jaqu .[m
[32m+[m[32m    environment:[m
[32m+[m[32m      GIT_PRIVATE_SSH:[m
[32m+[m[32m        from_secret: github_private_key[m
[32m+[m[32m    volumes:[m
[32m+[m[32m      - name: docker_sock[m
[32m+[m[32m        path: /var/run/docker.sock[m
[32m+[m
[32m+[m[32m  ################################################# End Nightly Steps ############################################################[m
[32m+[m
[32m+[m[32m---[m
[32m+[m
 ################################################### DEV Steps ########################################################[m
 [m
 kind: pipeline[m
