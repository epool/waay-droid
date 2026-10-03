package dev.epool.waay.di

import org.koin.core.module.Module

/**
 * Platform services: `ObservableSettings`, `Speaker` and `DeviceLocale`
 * (contracts/platform-services.md).
 */
internal expect val platformModule: Module
