FROM ubuntu:23.10

# Root user
USER 0

RUN rm /bin/sh && ln -s /bin/bash /bin/sh

RUN apt-get update \
  && apt-get install -y \
  curl \
  git \
  golang \
  sudo \
  vim \
  wget \
  unzip \
  zip \
  npm \
  nodejs \
  ca-certificates \
  gnupg \
  && rm -rf /var/lib/apt/lists/*

#### DOCKER

# Add Docker's official GPG key:
RUN sudo install -m 0755 -d /etc/apt/keyrings
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
RUN sudo chmod a+r /etc/apt/keyrings/docker.gpg

RUN sudo curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
RUN sudo chmod +x /usr/local/bin/docker-compose

# Add the repository to Apt sources:
RUN echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
RUN sudo apt-get update
RUN sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

#### NVM
ARG NODE_VERSION=v21.4.0

RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash

ENV NVM_DIR="/root/.nvm"

RUN . "$NVM_DIR/nvm.sh" && nvm install ${NODE_VERSION} && \
  nvm alias default ${NODE_VERSION} && \
  nvm use default

#### Github CLI

RUN curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg \
  && sudo chmod go+r /usr/share/keyrings/githubcli-archive-keyring.gpg \
  && echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null \
  && sudo apt update \
  && sudo apt install gh -y

RUN sudo apt update \
  && sudo apt install gh \
  && rm -rf /var/lib/apt/lists/*

#### Scala
ARG JDK_VERSION=21.0.1-tem
ARG SBT_VERSION=1.9.7
ARG SCALA_VERSION=3.3.1

RUN curl -s "https://get.sdkman.io" | bash
RUN sudo chmod a+x "/root/.sdkman/bin/sdkman-init.sh"
RUN source "/root/.sdkman/bin/sdkman-init.sh" \
  && sdk install java ${JDK_VERSION} \
  && sdk install sbt ${SBT_VERSION} \
  && sdk install scala ${SCALA_VERSION}

CMD sudo service start docker