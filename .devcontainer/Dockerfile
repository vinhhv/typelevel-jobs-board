FROM ubuntu:22.04

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
RUN sudo apt-get install -y docker-ce-cli

### Stripe
RUN curl -s https://packages.stripe.dev/api/security/keypair/stripe-cli-gpg/public | gpg --dearmor | sudo tee /usr/share/keyrings/stripe.gpg
RUN echo "deb [signed-by=/usr/share/keyrings/stripe.gpg] https://packages.stripe.dev/stripe-cli-debian-local stable main" | sudo tee -a /etc/apt/sources.list.d/stripe.list
RUN sudo apt update
RUN sudo apt install -y stripe

# Create non-root user and add to group to connect to host Docker daemon
RUN groupadd -r developer && useradd -r -g developer -G sudo -m -s /bin/bash developer && \
  groupadd -g 800 docker && \
  usermod -aG docker developer

# Switch to the non-root user
USER developer
ENV HOME /home/developer
# Set the working directory
WORKDIR /home/developer

#### NVM
ARG NODE_VERSION=v21.4.0

RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash

ENV NVM_DIR="$HOME/.nvm"

RUN . "$NVM_DIR/nvm.sh" && nvm install ${NODE_VERSION} && \
  nvm alias default ${NODE_VERSION} && \
  nvm use default

#### Scala
ARG JDK_VERSION=21.0.1-tem
ARG SBT_VERSION=1.9.7
ARG SCALA_VERSION=3.3.1

RUN curl -s "https://get.sdkman.io" | bash
RUN chmod a+x "$HOME/.sdkman/bin/sdkman-init.sh"
RUN source "$HOME/.sdkman/bin/sdkman-init.sh" \
  && sdk install java ${JDK_VERSION} \
  && sdk install sbt ${SBT_VERSION} \
  && sdk install scala ${SCALA_VERSION}
