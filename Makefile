.PHONY: install build run test clean pre-commit-install pre-commit-run venv

GRADLEW = ./gradlew
VENV_DIR = .pre-commit-venv
PYTHON = $(VENV_DIR)/bin/python
PIP = $(VENV_DIR)/bin/pip
PRE_COMMIT = $(VENV_DIR)/bin/pre-commit

install: venv pre-commit-install build
	@echo "Backend installation complete."

build:
	$(GRADLEW) build

run:
	$(GRADLEW) bootRun

test:
	$(GRADLEW) test

clean:
	$(GRADLEW) clean
	@echo "Cleaning pre-commit virtual environment..."
	rm -rf $(VENV_DIR)

venv:
	@echo "Creating Python virtual environment for pre-commit..."
	python3 -m venv $(VENV_DIR)
	@echo "Installing pre-commit in virtual environment..."
	$(PIP) install pre-commit

pre-commit-install:
	@echo "Installing pre-commit hooks..."
	$(PRE_COMMIT) install

pre-commit-run:
	@echo "Running pre-commit checks..."
	$(PRE_COMMIT) run --all-files