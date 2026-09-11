import { DownloadIcon, Loader2Icon } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import { api, errorMessage } from '@/api/client'
import { Button } from '@/components/ui/button'
import type { WithdrawalFilter } from '@/types'

/** Downloads exactly the rows the table shows, because it sends the same filter to the export endpoint. */
export function CsvDownloadButton({
  filter,
  disabled = false,
}: {
  filter: WithdrawalFilter
  disabled?: boolean
}) {
  const [downloading, setDownloading] = useState(false)

  async function handleClick() {
    setDownloading(true)
    try {
      const filename = await api.downloadStatement(filter)
      toast.success(`Downloaded ${filename}`)
    } catch (error) {
      toast.error('Download failed', { description: errorMessage(error) })
    } finally {
      setDownloading(false)
    }
  }

  return (
    <Button variant="outline" onClick={() => void handleClick()} disabled={disabled || downloading}>
      {downloading ? <Loader2Icon className="animate-spin" /> : <DownloadIcon />}
      Download CSV
    </Button>
  )
}
