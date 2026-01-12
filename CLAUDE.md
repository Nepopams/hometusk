# Claude Code Development Guide

This document provides context and guidelines for AI-assisted development on this project.

## Project Goals

> TODO: Define core business objectives and success metrics

> TODO: Define target users and use cases

> TODO: Define scope and non-goals

## Architecture Principles

> TODO: Define architectural patterns (microservices, event-driven, etc.)

> TODO: Define service communication patterns (REST, gRPC, message queues)

> TODO: Define data storage strategy (databases per service, shared, etc.)

> TODO: Define scalability and performance requirements

> TODO: Define security and compliance requirements

## Technology Stack

> TODO: Specify backend languages and frameworks

> TODO: Specify frontend frameworks and libraries

> TODO: Specify databases and caching layers

> TODO: Specify infrastructure and deployment platforms

> TODO: Specify monitoring and observability tools

## Coding and Decision-Making Rules

### General Principles
- Keep it simple – avoid premature optimization
- Code for maintainability and clarity
- Follow the principle of least surprise
- Document decisions in `docs/architecture/decisions/`

### Service and Contract Changes
**CRITICAL RULE:** When making changes to services, contracts, or pipelines, you MUST:
1. Update the service catalog (`docs/architecture/service-catalog.md`)
2. Update or create the relevant API contract in `docs/contracts/`
3. Create or update an Architecture Decision Record (ADR) in `docs/architecture/decisions/` if the change involves architectural decisions

This ensures consistency between code, documentation, and architectural decisions. Always check and update these artifacts as part of any service/contract/pipeline change.

### Code Quality
> TODO: Define code style and linting rules

> TODO: Define testing requirements (unit, integration, e2e)

> TODO: Define code review process

### API Design
> TODO: Define API versioning strategy

> TODO: Define error handling conventions

> TODO: Define authentication/authorization patterns

### Database
> TODO: Define migration strategy

> TODO: Define naming conventions

## Local Development

### Prerequisites
> TODO: List required tools and versions

### Environment Setup
> TODO: Environment variables and configuration

### Running Services
> TODO: How to start individual services

> TODO: How to run the full stack locally

### Development Workflow
> TODO: Branch naming conventions

> TODO: Commit message format

> TODO: Testing before commit

## Claude Code Tips

### Custom Commands
Custom Claude commands are located in `.claude/commands/`

> TODO: Document custom commands when created

### Project Context
Key context documents for Claude:
- Architecture diagrams: `docs/architecture/diagrams/`
- Architecture decisions: `docs/architecture/decisions/`
- API contracts: `docs/contracts/`

### Common Tasks
> TODO: Add common development tasks and how to ask Claude for help

## References

> TODO: Links to external documentation, tools, and resources
