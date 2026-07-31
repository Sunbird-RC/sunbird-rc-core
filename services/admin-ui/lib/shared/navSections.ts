import {
  Compass,
  Database,
  FileText,
  Shield,
  Inbox,
  Paperclip,
  UserPlus,
  Clock,
  Layers,
  Award,
  QrCode,
  Key,
  Image as ImageIcon,
} from 'lucide-react'
import type { Flags } from './types/flags'
import type { NavSection } from './types/nav'

// Mirrors the design's sidebar exactly: 4 groups, 12 screens, one icon each
// (Lucide — glyph-compatible with the Feather set the real product uses).
// Plus a 5th, admin-ui-only "Getting started" section (Pass 2 addition —
// not in the original design, which had no dashboard screen).
export function buildNavSections(flags: Flags): NavSection[] {
  return [
    {
      title: 'Overview',
      items: [{ id: 'getting-started', label: 'Getting started', href: '/', icon: Compass }],
    },
    {
      title: 'Registry core',
      items: [
        { id: 'entities', label: 'Entities', href: '/entities', icon: Database },
        { id: 'registry-schemas', label: 'Registry schemas', href: '/registry-schemas', icon: FileText },
        {
          id: 'policies',
          label: 'Attestation policies',
          href: '/policies',
          icon: Shield,
          disabledReason: flags.claims ? undefined : 'Needs CLAIMS_ENABLED=true',
        },
        {
          id: 'claims',
          label: 'Claim inbox',
          href: '/claims',
          icon: Inbox,
          disabledReason: flags.claims ? undefined : 'Needs CLAIMS_ENABLED=true',
        },
        {
          id: 'documents',
          label: 'Documents',
          href: '/documents',
          icon: Paperclip,
          disabledReason: flags.filestorage ? undefined : 'Needs FILESSTORAGE_ENABLED=true',
        },
        { id: 'invites', label: 'Invites', href: '/invites', icon: UserPlus },
        { id: 'audit', label: 'Audit trail', href: '/audit', icon: Clock },
      ],
    },
    {
      title: 'Credential schemas',
      items: [
        { id: 'schemas', label: 'Schemas', href: '/schemas', icon: Layers },
        { id: 'credentials', label: 'Credentials', href: '/credentials', icon: Award },
      ],
    },
    {
      title: 'Issuance',
      items: [
        {
          id: 'offers',
          label: 'OID4VCI offers',
          href: '/offers',
          icon: QrCode,
          disabledReason: flags.oid4vcReachable ? undefined : 'oid4vc-service is not running (compose profile "oid4vc")',
        },
      ],
    },
    {
      title: 'Identity',
      items: [
        { id: 'dids', label: 'DIDs', href: '/dids', icon: Key },
        { id: 'templates', label: 'Render templates', href: '/templates', icon: ImageIcon },
      ],
    },
  ]
}
