'use client'

import { forwardRef } from 'react'

export type ButtonVariant = 'default' | 'secondary' | 'outline' | 'ghost' | 'link' | 'destructive'
export type ButtonSize = 'sm' | 'default' | 'lg'

const SIZE_CLASSES: Record<ButtonSize, string> = {
  sm: 'h-8 px-3 text-xs',
  default: 'h-10 px-4 text-sm',
  lg: 'h-13 px-8 text-base',
}

// Matches the DS Button spec exactly: brick default (hover -> ginger),
// wave secondary (hover -> wave-shade), white outline (hover -> tint-97),
// brick ghost/link (hover -> tint-93), danger destructive.
const VARIANT_CLASSES: Record<ButtonVariant, string> = {
  default: 'bg-brick text-white shadow-sm hover:bg-ginger',
  secondary: 'bg-wave text-white shadow-sm hover:bg-wave-shade',
  outline: 'bg-white text-gray-900 border border-gray-200 shadow-sm hover:bg-tint-97',
  ghost: 'bg-transparent text-brick hover:bg-tint-93',
  link: 'bg-transparent text-brick underline underline-offset-[3px]',
  destructive: 'bg-danger text-white shadow-sm hover:bg-[#cf3c4f]',
}

export const Button = forwardRef<HTMLButtonElement, React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant
  size?: ButtonSize
  fullWidth?: boolean
}>(function Button({ variant = 'default', size = 'default', fullWidth = false, className = '', disabled, ...rest }, ref) {
  return (
    <button
      ref={ref}
      disabled={disabled}
      className={`inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-xs font-medium transition-colors duration-200 ease-out disabled:cursor-not-allowed disabled:opacity-50 ${SIZE_CLASSES[size]} ${VARIANT_CLASSES[variant]} ${fullWidth ? 'w-full' : ''} ${className}`}
      {...rest}
    />
  )
})
